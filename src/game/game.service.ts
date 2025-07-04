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
import { plainToInstance } from 'class-transformer';
import { connect } from 'http2';

@Injectable()
export class GameService {
  constructor(
    private prisma: PrismaService,
    private deckService: DeckService,
    private playerService: PlayerService,
    private handService: HandService,
    private cardService: CardService,
  ) { }

  //TODO: transaction vse metode kjer se dogaja vec Prisma callov
  //TODO: remove vse metode (in routes) ki jih ne uporabljas

  async create(data: CreateGameDto): Promise<Game> {
    //transaction in two pieces 
    const game = await this.prisma.$transaction(async (tx) => {
      // 1. Create the Game and related decks/cards
      const game = await tx.game.create({
        data: {
          decks: {
            create: data.decks.map(deck => ({
              size: deck.size,
              cards: {
                create: deck.cards
                  .filter(card => card !== null)
                  .map(card => ({
                    priority: card!.priority,
                    value: card!.value,
                    color: card!.color,
                    texture: card!.texture,
                  })),
              },
            })),
          },
          players: {
            connectOrCreate: data.players
              .filter(player => player !== null)
              .map(player => ({
                where: { id: player.id || 0 },
                create: {
                  name: player.name,
                  score: player.score,
                },
              })),
          },
          maxPlayers: data.maxPlayers,
          gameState: data.gameState,
          currentTurn: data.currentTurn,
          turnOrder: data.turnOrder,
        },
        include: {
          decks: {
            include: {
              cards: true,
            },
          },
        },
      });

      // 2. Get last card from first created deck
      const discardDeck = game.decks[1].cards.filter(card => card !== null);  //discard deck
      //console.log("CARDS: ",discardDeck)
      const lastCard = discardDeck[discardDeck.length - 1];

      if (!lastCard) {
        throw new Error('No card found in the first deck to set as topCard');
      }

      // 3. Update game with topCard
      const updatedGame = await tx.game.update({
        where: { id: game.id },
        data: {
          topCard: {
            connect: { id: lastCard.id },
          },
        },
        include: {
          topCard: true,
          decks: { include: { cards: true } },
          players: true,
        },
      });

      return updatedGame;
    })

    return await this.prisma.$transaction(async (prisma) => {
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
              await this.handService.update(player.hand,player.id)
              /*
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
              */
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
    })
  }



  async createNew(): Promise<Game> {
    return await this.prisma.game.create({})
  }

  async getAll(): Promise<Game[] | null> {
    const games = await this.prisma.game.findMany({
      include: {
        //decks: true,
        players: true,
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
        topCard: true,
        /*
        gameState: true,
        currentTurn: true,
        turnOrder: true,
        */
      },
    })
    if (game != null) {
      console.log("GAME:", game)
      console.log("TopCard:", game.topCard)
      //console.log("GAME decks draw:", game.decks[0])
      //console.log("GAME decks discard:", game.decks[1])
    }
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
    console.log("PLAYERS: ")
    for (const player of players)
      console.log(`id: ${player.id} name: ${player.name}`)
    return players
  }

  async getTurnAndTopCard(id: number): Promise<
    Game | null> {
    const game = await this.prisma.game.findUnique({
      where: { id },
      include: {
        topCard: true,
      }
    })

    if (!game)
      throw new NotFoundException(`Game with id ${id} not found!`);

    console.log(`TURN: ${game.currentTurn} TOPCARD: ${game.topCard.texture} STATE: ${game.gameState}`)
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

  async update(id: number, dto: UpdateGameDto): Promise<Game> {
    const gameToUpdate = await this.get(id);
    if (!gameToUpdate)
      throw new NotFoundException(`Game with id ${id} not found!`)

    //run all updates as a transaction
    return await this.prisma.$transaction(async (prisma) => {
      if (dto.decks && dto.decks[1]) {
        //IF TOP CARD IS RAINBOW: UPDATE TO SELECTED DEFAULT VALUE
        const discardDeck = dto.decks[1].cards.filter(card => card !== null);  //discard deck
        //console.log("CARDS: ",discardDeck)
        const lastCard = discardDeck[discardDeck.length - 1];
        const newColor = dto.topCard.color
        const newTexture = dto.topCard.texture

        if (lastCard.color == "-" && lastCard.texture == "rainbow") {
          console.log("Top Card: ", dto.topCard)
          console.log("dicard card: ", lastCard)
          const updatedCard = await this.cardService.update(lastCard.id, { color: newColor, texture: newTexture })
          dto.topCard = updatedCard
          console.log("Updated top card: ", dto.topCard)
        }
      }

      //first Update Decks
      if (dto.decks) {
        const gameDecks = await this.deckService.getForGame(id)
        await this.deckService.updateForGame(dto.decks, gameDecks)
      }
      //second Update Players' Hands
      if (dto.players) {
        //refetch before updating (important for first game turn else unsynchronized behaviour)
        const gamePlayers = await this.playerService.getForGame(id)

        //keep newly updated index values: firstIndex & lastIndex of Hands and merge them into gamePlayers data
        for (const updatedPlayer of dto.players) {
          const targetPlayer = gamePlayers.find(p => p.id === updatedPlayer.id);
          if (targetPlayer && targetPlayer.hand && updatedPlayer.hand) {
            targetPlayer.hand.indexFirst = updatedPlayer.hand.indexFirst;
            targetPlayer.hand.indexLast = updatedPlayer.hand.indexLast;
          }
        }

        //update hands
        await this.handService.updateForGame(dto.players, gamePlayers)
        
        const updatedHands = await this.playerService.getForGame(id)
        console.log("Updated hands 0: ",updatedHands.at(0).hand.cards)
        console.log("Updated hands 1: ",updatedHands.at(1).hand.cards)
        
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
      if (dto.topCard !== undefined && dto.topCard?.id) updateData.topCard = { connect: { id: dto.topCard.id } }

      //Sometimes if creating a Game with only one Player and leaving right after, the Game is deleted but
      //this method will execute as if the object still exists and throw error when trying to update
      try {
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
            topCard: true,
          },
        });

        /*
        console.log("PLAYER 0: ")
        for (const player of game.players[0].hand.cards)
          console.log(`${player.id} ${player.color} ${player.value} ${player.texture}`)
  
        console.log("PLAYER 1: ")
        for (const player of game.players[1].hand.cards)
          console.log(`${player.id} ${player.color} ${player.value} ${player.texture}`)
        */

        return game;
      }
      catch (error) {
        console.error('Game update failed. Possibly because game is already deleted.');
        //throw error; // re-throw or wrap it
        return null;
      }
    })
  }


  //ADD PLAYER TO GAME
  async updatePlayerAdd(id: number, dto: UpdatePlayerDto): Promise<Game> {
    console.log("ADDING PLAYER WITH HAND: ", dto.hand.cards)
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

        //get updated game
        const updatedGamePlayers = await this.getPlayers(id)

        //one player remains, change state to Paused
        if (updatedGamePlayers.length >= 2) {
          console.log("2 OR MORE PLAYERS, CHANGE GAMESTATE")
          //use class-transformer to pass dto with only gameState variable
          const stateDto = plainToInstance(UpdateGameDto, {
            gameState: "Running",
          });
          return await this.update(id, stateDto)
        }
      }

      return await this.get(id)
    })
  }

  //REMOVE PLAYER FROM GAME
  async updatePlayerRemove(gameId: number, playerId: number, dto: UpdatePlayerDto): Promise<Game | null> {
    //run update as transaction
    await this.prisma.$transaction(async (prisma) => {
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
      await this.prisma.game.update({
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
    })

    //check game state after the fact
    const returnedPlayers = await this.getPlayers(gameId)

    //one player remains, change state to Paused
    if (returnedPlayers.length == 1) {
      console.log("ONLY 1 PLAYER LEFT, CHANGE GAMESTATE")
      //use class-transformer to pass dto with only gameState variable
      const stateDto = plainToInstance(UpdateGameDto, {
        gameState: "Paused",
      });
      return await this.update(gameId, stateDto)
    }

    //check if game has no more active players and is over
    //in this case, delete game and all its connected ids (player is not deleted)
    if (returnedPlayers.length == 0) {
      console.log("NO PLAYERS, GAME SHOULD BE DELETED")
      await this.delete(gameId)
      return null
    }

    return await this.get(gameId)
  }

  async delete(id: number): Promise<Game> {
    return await this.prisma.$transaction(async (prisma) => {
      //delete cards
      await this.cardService.deleteManyFromGame(id)
      //delete decks
      await this.deckService.deleteManyFromGame(id)

      //TODO?: delete hands?

      console.log("Deleting Game", id)
      return this.prisma.game.delete({
        where: { id },
      })
    })
  }
}
