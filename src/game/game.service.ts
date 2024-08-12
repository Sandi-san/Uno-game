import { Prisma, Game, Player } from '@prisma/client';
import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateGameDto } from './dto/create-game.dto';
import { DeckService } from 'src/deck/deck.service';
import { PlayerService } from 'src/player/player.service';
import { DeckDto } from 'src/deck/dto/create-deck.dto';
import { CardDto } from 'src/card/dto/create-card.dto';
import { PlayerDto } from 'src/player/dto/create-player.dto';

@Injectable()
export class GameService {
  constructor(
    private prisma: PrismaService,
    private deckService: DeckService,
    private playerService: PlayerService,
  ) { }

  async create(data: CreateGameDto): Promise<Game> {
    const game = await this.prisma.game.create({
      data: {
        decks: {
          create: data.decks.map((deck: DeckDto) => ({
            size: deck.size,
            cards: {
              create: deck.cards
                .filter((card: CardDto | null) => card !== null) // Filter out null values
                .map((card: CardDto) => ({
                  priority: card.priority,
                  value: card.value,
                  color: card.color,
                  texture: card.texture,
                }))
            },
          }))
        },
        players: {
          create: data.players
            .filter((player: PlayerDto | null) => player !== null) // Filter out null values
            .map((player: PlayerDto) => ({
              name: player.name,
              score: player.score,
              hand: player.hand ? {
                create: {
                  indexFirst: player.hand.indexFirst,
                  indexLast: player.hand.indexLast,
                  cards: {
                    create: player.hand.cards
                      .filter((card: CardDto | null) => card !== null) // Filter out null values
                      .map((card: CardDto) => ({
                        priority: card.priority,
                        value: card.value,
                        color: card.color,
                        texture: card.texture,
                      }))
                  },
                },
              } : undefined,
            }))
        },
        maxPlayers: data.maxPlayers,
        topCard: data.topCard ? {
          create: {
            priority: data.topCard.priority,
            value: data.topCard.value,
            color: data.topCard.color,
            texture: data.topCard.texture,
          },
        } : null,
      }
    });
    console.log("GAME CREATED:",game)
    return game
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
    })
    console.log(games)
    return games
  }

  async get(id: number): Promise<Game | null> {
    return this.prisma.game.findUnique({
      where: { id },
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

    if(!game)
      return null

    const players = (await game).players
    console.log("PLAYERS:",players)
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
