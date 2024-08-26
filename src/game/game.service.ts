import { Card, Deck, Game, Hand, Player } from '@prisma/client';
import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateGameDto } from './dto/create-game.dto';
import { DeckService } from 'src/deck/deck.service';
import { PlayerService } from 'src/player/player.service';
import { CreateDeckDto } from 'src/deck/dto/create-deck.dto';
import { CreateCardDto } from 'src/card/dto/create-card.dto';
import { HandService } from 'src/hand/hand.service';
import { UpdateGameDto } from './dto/update-game.dto';
import { CreatePlayerDto } from 'src/player/dto/create-player.dto';
import { UpdatePlayerDto } from 'src/player/dto/update-player.dto';
import { UpdateDeckDto } from 'src/deck/dto/update-deck.dto';
import { CardService } from 'src/card/card.service';

@Injectable()
export class GameService {
  constructor(
    private prisma: PrismaService,
    private deckService: DeckService,
    private playerService: PlayerService,
    private handService: HandService,
    private cardService: CardService,
  ) { }

  async create(data: CreateGameDto): Promise<Game> {
    const game = await this.prisma.game.create({
      data: {
        decks: {
          create: data.decks.map((deck: CreateDeckDto) => ({
            size: deck.size,
            cards: {
              create: deck.cards
                .filter((card: CreateCardDto | null) => card !== null)
                .map((card: CreateCardDto) => ({
                  priority: card.priority,
                  value: card.value,
                  color: card.color,
                  texture: card.texture,
                })),
            },
          })),
        },
        players: {
          connectOrCreate: data.players
            .filter((player: CreatePlayerDto | null) => player !== null)
            .map((player: CreatePlayerDto) => ({
              where: { id: player.id || 0 }, // Ensure player.id is valid
              create: {
                name: player.name,
                score: player.score
              },
            })),
        },
        maxPlayers: data.maxPlayers,
        gameState: data.gameState,
        currentTurn: data.currentTurn,
        turnOrder: data.turnOrder,
      },
    });

    // After creating the game, update the gameId and hand for each player
    for (const player of data.players) {
      if (player && player.id) {
        // Update gameId
        await this.prisma.player.update({
          where: { id: player.id },
          data: {
            gameId: game.id,
            joinedAt: new Date()
          },
        });

        // If hand data is provided, handle the hand creation or update
        if (player.hand) {
          const existingHand = await this.prisma.hand.findUnique({
            where: { playerId: player.id },
          });

          if (existingHand) {
            // Update the existing hand
            //TODO: klic handService update()
            await this.prisma.hand.update({
              where: { playerId: player.id },
              data: {
                indexFirst: player.hand.indexFirst,
                indexLast: player.hand.indexLast,
                cards: {
                  deleteMany: {}, // Optionally delete old cards
                  create: player.hand.cards
                    .filter((card: CreateCardDto | null) => card !== null)
                    .map((card: CreateCardDto) => ({
                      priority: card.priority,
                      value: card.value,
                      color: card.color,
                      texture: card.texture,
                    })),
                },
              },
            });
          } else {
            //create new hand
            await this.handService.create(player.hand, player.id);
          }
        }
      }
    }

    const newGame = await this.get(game.id);
    console.log('GAME CREATED:', newGame);
    return newGame;
  }



  async createNew(): Promise<Game> {
    return await this.prisma.game.create({})
  }

  //TODO: can not include decks, hand
  async getAll(): Promise<Game[] | null> {
    const games = await this.prisma.game.findMany({
      include: {
        decks: true,
        players: {
          include: {
            hand: {
              include: {
                cards: true,
              },
            },
          },
        },
      },
      orderBy: {
        createdAt: 'desc',
      }
    })
    console.log(games)
    return games
  }

  async get(id: number): Promise<Game | null> {
    const game = await this.prisma.game.findUnique({
      where: { id },
      include: {
        decks: {
          include: {
            cards: true
          }
        },
        players: {
          include: {
            hand: {
              include: {
                cards: true,
              },
            },
          },
          orderBy: {
            joinedAt: 'asc',
          }
        },
        /*
        gameState: true,
        currentTurn: true,
        turnOrder: true,
        */
      },
    })
    console.log("GAME:", game)
    return game
  }

  async getPlayers(id: number): Promise<Player[] | null> {
    const game = await this.prisma.game.findUnique({
      where: { id },
      include: {
        players: {
          include: {
            hand: {
              include: {
                cards: true,
              },
            },
          },
        },
      },
    })

    if (!game)
      throw new NotFoundException(`Game with id ${id} not found!`);

    const players = game.players
    console.log("PLAYERS:", players)
    return players
  }

  //TODO: get 2nd Deck of game (discard Deck and return)
  async getTurnAndDiscardDeck(id: number): Promise<
    Game | null> {
    const game = await this.prisma.game.findUnique({
      where: { id },
      include: { decks: { include: { cards: true } } }
    })

    if (!game)
      throw new NotFoundException(`Game with id ${id} not found!`);

    const topCard = game.decks[1].cards[game.decks[1].cards.length-1]
    console.log(`TURN: ${game.currentTurn} TOPCARD: ${topCard.texture} STATE: ${game.gameState}`)
    return game
  }

  //GET function, return only deck, player and hand ids
  async getIds(id: number): Promise<Game & {
    decks: (Deck & { cards: Card[] })[],
    players: (Player & { hand: Hand & { cards: Card[] } })[]
  } | null> {
    const game = await this.prisma.game.findUnique({
      where: { id },
      include: {
        decks: {  // Include decks
          include: {
            cards: true
          }
        },
        players: {
          include: {
            hand: {
              include: {
                cards: true
              }
            }
          },
        },
      },
    })
    //console.log(game)
    return game
  }

  //TODO?: UPDATE GAME RAZEN PLAYERJEV
  async update(id: number, dto: UpdateGameDto): Promise<Game> {
    const gameToUpdate = await this.getIds(id);
    if (!gameToUpdate)
      throw new NotFoundException(`Game with id ${id} not found!`)

    /*
    // Update Decks
    if (dto.decks) {
      await Promise.all(dto.decks.map(async (deckDto) => {
        const deckToUpdate = gameToUpdate.decks.find(d => d.id === deckDto.id);
        if (!deckToUpdate) throw new BadRequestException(`Deck with id ${deckDto.id} not found`);

        // Call your service to update decks
        await this.deckService.updateForGame(id, [deckDto], [deckToUpdate]);
      }));
    }

    // Update Hands
    if (dto.players) {
      await Promise.all(dto.players.map(async (playerDto) => {
        const playerToUpdate = gameToUpdate.players.find(p => p.id === playerDto.id);
        if (!playerToUpdate) throw new BadRequestException(`Player with id ${playerDto.id} not found`);

        // Call your service to update hands
        await this.handService.updateForGame(id, [playerDto], [playerToUpdate]);
      }));
    }
    */

    //run all updates as a transaction
    return await this.prisma.$transaction(async (prisma) => {
      //first Update Decks
      if (dto.decks) {
        await this.deckService.updateForGame(id, dto.decks, gameToUpdate.decks)
      }
      //second Update Players' Hands
      if (dto.players) {
        await this.handService.updateForGame(id, dto.players, gameToUpdate.players)
      }

      //if the game is over, update all connected players' scores
      if (dto.gameState == 'Over' && dto.players) {
        for (const player of dto.players)
          await this.playerService.updateScore(player.id, player.score)
      }

      const updateData: any = {};

      // Other updates
      if (dto.maxPlayers !== undefined) updateData.maxPlayers = dto.maxPlayers;
      if (dto.gameState !== undefined) updateData.gameState = dto.gameState;
      if (dto.currentTurn !== undefined) updateData.currentTurn = dto.currentTurn;
      if (dto.turnOrder !== undefined) updateData.turnOrder = dto.turnOrder;

      const game = await this.prisma.game.update({
        where: { id },
        data: updateData,
        include: {
          decks: { include: { cards: true } },
          players: {
            include: {
              hand: { include: { cards: true } },
            },
            orderBy: {
              joinedAt: 'asc',
            }
          },
        },
      });

      return game;
    })
  }


  //ADD PLAYER TO GAME
  async updatePlayerAdd(id: number, dto: UpdatePlayerDto): Promise<Game> {
    //run update as transaction
    return await this.prisma.$transaction(async (prisma) => {
      //check if player data sent: update player's gameId
      if (dto) {
        //update player and return
        const updatedPlayer = await this.playerService.update(dto, id)

        //get game including ids of decks
        const game = await this.getIds(id)
        if (!game) throw new NotFoundException(`Game with id ${id} not found!`);

        //create a deck dto of the draw deck filled with cards from the updated player hands
        const deckDto: UpdateDeckDto = {
          id: game.decks[0].id,
          size: game.decks[0].size,
          cards: updatedPlayer.hand.cards,
          gameId: id
        }

        //console.log("DECK:", deckDto)
        //remove cards from the decks that are now in player's hand
        await this.deckService.updateRemoveCards(deckDto)
      }

      //return the game
      return this.get(id)
    })
  }

  //REMOVE PLAYER FROM GAME
  async updatePlayerRemove(gameId: number, playerId: number, dto: UpdatePlayerDto): Promise<Game | null> {
    //run update as transaction
    return await this.prisma.$transaction(async (prisma) => {
      //get ids of objects connected to game
      const game = await this.getIds(gameId)
      if (!game) throw new NotFoundException(`Game with id ${gameId} is invalid!`);

      //get player from game to delete
      const player = game.players.find(p => p.id === playerId);
      if (!player) throw new NotFoundException(`Player with id ${playerId} not found in game with id ${gameId}`);

      //move player's cards from hand and move to drawDeck of game
      //move only if game isnt already over
      if (game.gameState != "Over") {
        const drawDeck = game.decks[0]
        const playerCards = player.hand.cards

        //update deck with player's cards
        await this.deckService.updateAddCards(drawDeck, playerCards)
      }
      //game is over: update player's score before removing him from game
      else {
        await this.playerService.updateScore(dto.id, dto.score)
      }

      //remove player from game
      const updatedGame = await this.prisma.game.update({
        where: { id: gameId },
        data: {
          players: {
            disconnect: { id: playerId }
          }
        },
        include: {
          players: true
        }
      })

      //update player (remove joinedAt, gameId and Hand)
      const updatedPlayerDto = {
        ...dto,
        gameId: -1, // Remove association with game
        joinedAt: -1, // Remove joinedAt
        hand: null, // Remove hand object (handled in your service update logic)
      };

      await this.playerService.update(updatedPlayerDto, gameId)

      //check if game has no more active players and is over
      //in this case, delete game and all its connected ids (player is not deleted)
      if (updatedGame.players.length == 0 && updatedGame.gameState == "Over") {
        await this.delete(gameId)
        return null
      }

      //return the game
      return this.get(gameId)
    })
  }

  async delete(id: number): Promise<Game> {
    return await this.prisma.$transaction(async (prisma) => {
      //delete cards
      await this.cardService.deleteManyFromDeck(id)
      //delete decks
      await this.deckService.deleteMany(id)

      console.log("Deleting Game", id)
      return this.prisma.game.delete({
        where: { id },
      })
    })
  }
}
