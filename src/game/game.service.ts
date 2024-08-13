import { Prisma, Game, Player } from '@prisma/client';
import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateGameDto } from './dto/create-game.dto';
import { DeckService } from 'src/deck/deck.service';
import { PlayerService } from 'src/player/player.service';
import { DeckDto } from 'src/deck/dto/create-deck.dto';
import { CardDto } from 'src/card/dto/create-card.dto';
import { PlayerDto } from 'src/player/dto/create-player.dto';
import { HandService } from 'src/hand/hand.service';

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
          create: data.decks.map((deck: DeckDto) => ({
            size: deck.size,
            cards: {
              create: deck.cards
                .filter((card: CardDto | null) => card !== null)
                .map((card: CardDto) => ({
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
            .filter((player: PlayerDto | null) => player !== null)
            .map((player: PlayerDto) => ({
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
                    .filter((card: CardDto | null) => card !== null)
                    .map((card: CardDto) => ({
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
    console.log(games.at(0).decks)
    //console.log(`games}\n${games.at(0).decks}`)
    return games
  }

  async get(id: number): Promise<Game | null> {
    const game = await this.prisma.game.findUnique({
      where: { id },
      include: {
        decks: {
          include:{
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

  async update(id: number, data: Prisma.GameUpdateInput): Promise<Game> {
    return this.prisma.game.update({
      where: { id },
      data,
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
    })
  }

  async delete(id: number): Promise<Game> {
    return this.prisma.game.delete({
      where: { id },
    })
  }
}
