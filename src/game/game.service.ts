import { Card, Deck, Game, Hand, Player } from '@prisma/client';
import { BadRequestException, Injectable } from '@nestjs/common';
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

@Injectable()
export class GameService {
  constructor(
    private prisma: PrismaService,
    private deckService: DeckService,
    private playerService: PlayerService,
    private handService: HandService,
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
                score: player.score,
                joinedAt: new Date()
              },
            })),
        },
        maxPlayers: data.maxPlayers,
        topCard: data.topCard
          ? {
            create: {
              priority: data.topCard.priority,
              value: data.topCard.value,
              color: data.topCard.color,
              texture: data.topCard.texture,
            },
          }
          : null,
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
          data: { gameId: game.id },
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

    const newGame = this.get(game.id);
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
        topCard: true,
        /*
        gameState: true,
        currentTurn: true,
        turnOrder: true,
        */
      },
    })
    console.log(game)
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
      throw new BadRequestException(`Id ${id} is invalid!`);

    const players = game.players
    console.log("PLAYERS:", players)
    return players
  }

  async getTurn(id: number): Promise<number | null> {
    const game = await this.prisma.game.findUnique({
      where: { id }
    })

    if (!game)
      throw new BadRequestException(`Id ${id} is invalid!`);

    const turn = game.currentTurn
    console.log("TURN:", turn)
    return turn
  }

  //GET function, return only deck, player and hand ids
  async getIds(id: number): Promise<Game & {
    decks: (Deck & { cards: Card[] } )[],
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
    console.log(game)
    return game
  }

  //TODO?: UPDATE GAME RAZEN PLAYERJEV
  async update(id: number, dto: UpdateGameDto): Promise<Game> {
    const gameToUpdate = await this.getIds(id);
    if (!gameToUpdate) throw new BadRequestException(`Id ${id} is invalid!`);
  
    const updateData: any = {};
  
    // Update Decks
    if (dto.decks) {
      updateData.decks = {
        update: gameToUpdate.decks.map((existingDeck, index) => {
          const deckDto = dto.decks[index];
          return {
            where: { id: existingDeck.id },
            data: {
              size: deckDto.size,
              cards: {
                disconnect: existingDeck.cards
                .filter(card => card && card.id !== undefined)
                .map(card => ({ id: card.id })),  // Disconnect
                connect: deckDto.cards
                .filter(card => card && card.id !== undefined)
                .map(card => ({ id: card.id })),  // Reconnect
              },
            },
          };
        }),
      };
    }
  
    // Update Players and Hands
    if (dto.players) {
      updateData.players = {
        update: dto.players.map(player => {
          const existingPlayer = gameToUpdate.players.find(p => p.id === player.id);
          if (!existingPlayer) throw new BadRequestException(`Player with id ${player.id} not found`);
  
          const newCardIds = player.hand.cards
            .filter(card => card && card.id !== undefined)
            .map(card => card.id);
          const existingCardIds = existingPlayer.hand.cards
            .filter(card => card && card.id !== undefined)
            .map(card => card.id);
  
          return {
            where: { id: player.id },
            data: {
              score: player.score,
              hand: {
                update: {
                  cards: {
                    disconnect: existingCardIds
                    .filter(id => !newCardIds.includes(id))
                    .map(id => ({ id })),
                    connect: newCardIds
                    .filter(id => !existingCardIds.includes(id))
                    .map(id => ({ id })),
                  },
                },
              },
            },
          };
        }),
      };
    }
  
    // Update topCard
    if (dto.topCard && dto.topCard.id !== undefined) {
      const topCardExists = await this.prisma.card.findUnique({
        where: { id: dto.topCard.id },
      });
      if (!topCardExists) throw new BadRequestException(`Top card with id ${dto.topCard.id} not found`);
      updateData.topCard = { connect: { id: dto.topCard.id } };
    }
  
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
        topCard: true,
      },
    });
  
    return game;
  }
  

  //ADD PLAYER TO GAME
  async updatePlayer(id: number, dto: UpdatePlayerDto): Promise<Game> {
    //check if player data sent: update player's gameId
    if (dto)
      await this.playerService.update(dto, id)

    //return the game
    return this.get(id)
  }

  async delete(id: number): Promise<Game> {
    return this.prisma.game.delete({
      where: { id },
    })
  }
}
