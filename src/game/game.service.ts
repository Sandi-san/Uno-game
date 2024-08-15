import { Deck, Game, Hand, Player } from '@prisma/client';
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

    console.log('GAME CREATED:', game);
    return game;
  }



  async createNew(): Promise<Game> {
    return await this.prisma.game.create({})
  }

  //TODO: can remove decks, hand
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
    const game = this.prisma.game.findUnique({
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
      return null

    const players = (await game).players
    console.log("PLAYERS:", players)
    return players
  }

  //GET function, return only deck and hand ids
  async getIds(id: number): Promise<Game & 
  { decks: Deck[], players: (Player & 
    { hand: Hand })[] } | null> {
    const game = await this.prisma.game.findUnique({
      where: { id },
      include: {
        decks: true,  // Include decks
        players: {
          include: {
            hand: true,
          },
        },
      },
    })
    console.log(game)
    return game
  }

  //UPDATE GAME RAZEN PLAYERJEV
  async update(id: number, dto: UpdateGameDto): Promise<Game> {
    const gameToUpdate = await this.getIds(id)

    if (!gameToUpdate) {
      throw new BadRequestException(`Id ${id} is invalid!`);
    }

    const updateData: any = {}

    // Step 2: Update decks if provided in dto
    if (dto.decks) {
      updateData.decks = {
        update: gameToUpdate.decks.map((existingDeck, index) => {
          const deckDto = dto.decks[index];
          return {
            where: { id: existingDeck.id },  // Use the existing deck ID
            data: {
              size: deckDto.size,  // Update the size if needed
              cards: {
                set: deckDto.cards
                  .filter(card => card && card.id !== undefined)
                  .map(card => ({ id: card.id })),
              },
            },
          };
        }),
      };
    }

    // Update players if provided in dto
    if (dto.players) {
      updateData.players = {
        update: dto.players.map((player) => {
          const existingPlayer = gameToUpdate.players.find(p => p.id === player.id);
          if (!existingPlayer) {
            throw new Error(`Player with id ${player.id} not found in game`);
          }
          return {
            where: { id: player.id },  // Use the existing player ID
            data: {
              score: player.score,
              hand: {
                update: {
                  cards: {
                    set: player.hand.cards
                      .filter(card => card && card.id !== undefined)
                      .map(card => ({ id: card.id })),
                  },
                },
              },
            }
          }
        })
      }
    }

    if (dto.topCard && dto.topCard.id !== undefined) {
      updateData.topCard = { connect: { id: dto.topCard.id } };
    }

    if (dto.maxPlayers !== undefined) updateData.maxPlayers = dto.maxPlayers;
    if (dto.gameState !== undefined) updateData.gameState = dto.gameState;
    if (dto.currentTurn !== undefined) updateData.currentTurn = dto.currentTurn;
    if (dto.turnOrder !== undefined) updateData.turnOrder = dto.turnOrder;

    const game = await this.prisma.game.update({
      where: { id },
      data: updateData,
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
        topCard: true,
      },
    });

    console.log(game)
    return game
  }

  async delete(id: number): Promise<Game> {
    return this.prisma.game.delete({
      where: { id },
    })
  }
}
