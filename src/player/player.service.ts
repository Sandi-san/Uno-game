import { Injectable, NotFoundException } from '@nestjs/common'
import { Card, Hand, Player } from '@prisma/client'
import { PrismaService } from 'src/prisma/prisma.service'
import { CreatePlayerDto } from './dto/create-player.dto';
import { CreateCardDto } from 'src/card/dto/create-card.dto';
import { UpdatePlayerDto } from './dto/update-player.dto';
import { CardService } from 'src/card/card.service';

@Injectable()
export class PlayerService {
  constructor(
    private prisma: PrismaService,
    private cardService: CardService,
  ) { }

  //TODO: GET PLAYER: IF EXISTS, UPDATE; ELSE: CREATE
  async create(data: CreatePlayerDto): Promise<Player> {
    const player = await this.prisma.player.create({
      data: {
        name: data.name,
        score: data.score,
        //hand can be null (set hand as null if not passed)
        hand: data.hand != null ? {
          create: {
            indexFirst: data.hand.indexFirst,
            indexLast: data.hand.indexLast,
            cards: {
              create: data.hand.cards
                .filter((card: CreateCardDto | null) => card !== null) // Filter out null values
                .map((card: CreateCardDto) => ({
                  priority: card.priority,
                  value: card.value,
                  color: card.color,
                  texture: card.texture,
                }))
            },
          },
        } : undefined,
        //set gameId if provided, also set joinedAt for the current game
        ...(data.gameId !== undefined && {
          gameId: data.gameId,
          joinedAt: new Date()
        })
      }
    })
    console.log(player)
    return player
  }

  //get player and hand and cards
  async get(id: number): Promise<Player &
  {
    hand: (Hand & { cards: Card[] })
  }
    | null> {
    return this.prisma.player.findUnique({
      where: { id },
      include: {
        hand: {
          include: {
            cards: true
          }
        }
      }
    })
  }

  async getForGame(gameId: number): Promise<(Player & { hand: Hand & { cards: Card[] } })[]> { //array return syntax
    return this.prisma.player.findMany({
      where: { gameId },
      include: {
        hand: {
          include: {
            cards: true
          }
        }
      }
    })
  }

  //get player by name and return hand
  async getByName(name: string): Promise<Player | null> {
    const player = await this.prisma.player.findFirst({
      where: { name },
      include: {
        hand: true
      }
    })
    console.log(player)
    return player
  }

  //get all players by descending scores
  async getPlayersScores(): Promise<Player[] | null> {
    const players = await this.prisma.player.findMany({
      orderBy: {
        score: 'desc',
      }
    })
    console.log(players)
    return players
  }

  //UNUSED: update players data & gameId
  async updateMany(players: UpdatePlayerDto[], gameId: number): Promise<Player[]> {
    const updatedPlayers = await Promise.all(
      players
        .filter((player: UpdatePlayerDto | null) => player !== null)
        .map(async (player) => {
          return this.prisma.player.update({
            where: {
              id: player.id,
            },
            data: {
              gameId, //change gameId
              /*
              name
              score: player.score,
              hand: {
                update: {
                  indexFirst: player.hand.indexFirst,
                  indexLast: player.hand.indexLast,
                  cards: {
                    set: player.hand.cards
                      .filter(card => card && card.id !== undefined)
                      .map(card => ({ id: card.id })),
                  },
                },
              }
                */
            },
            include: {
              hand: {
                include: {
                  cards: true
                }
              }
            }
          })
        })
    )
    console.log("UPDATED PLAYERS")
    for (const player in updatedPlayers) {
      console.log(player)
    }
    return updatedPlayers;
  }

  //update one player's data (Hands) & gameId
  async update(dto: UpdatePlayerDto, gameId: number):
    Promise<(Player & { hand: Hand & { cards: Card[] } })> {
    const player = await this.get(dto.id)

    if (!player)
      throw new NotFoundException(`Player with id ${dto.id} not found.`);

    const handData = dto.hand ? {
      indexFirst: dto.hand?.indexFirst ?? 0, // Default to 0 if not provided
      indexLast: dto.hand?.indexLast ?? -1,  // Default to -1 if not provided
      cards: {
        connect: dto.hand?.cards
          ?.filter(card => card && card.id !== undefined)
          .map(card => ({ id: card.id })) || [], // Handle case where cards might be undefined
      },
    } : null

    const updateData: any = {}

    if (dto.gameId == -1) {
      updateData.joinedAt = null
      updateData.gameId = null
    }
    else {
      updateData.joinedAt = new Date()
      updateData.gameId = gameId
    }

    // Handle hand update or deletion
    if (handData) {
      // If hand data is provided, update or create the hand
      updateData.hand = player.hand ? { update: handData } : { create: handData };
    } else if (player.hand) {
      //delete cards from hand
      //await this.cardService.deleteManyFromHand(player.hand.id)
      // If hand is not provided and player already has a hand, delete it
      updateData.hand = { delete: true };
    }

    console.log("PLAYER UPDATE DTO: ", updateData)

    const updatedPlayer = await this.prisma.player.update({
      where: {
        id: dto.id,
      },
      data: updateData,
      include: {
        hand: {
          include: {
            cards: true
          }
        }
      }
    })

    console.log("UPDATED PLAYER", updatedPlayer)
    return updatedPlayer;
  }

  async updateScore(id: number, score: number): Promise<Player> {
    const player = await this.get(id)
    if (!player)
      throw new NotFoundException(`Player with id ${id} not found.`);

    //console.log(`Player ${id} score is ${score}`)
    if (score > player.score) {
      const updatedPlayer = await this.prisma.player.update({
        where: { id },
        data: { score }
      })
      console.log(`Player ${id} changed score to ${score}`)
      return updatedPlayer
    }
    return player
  }

  async delete(id: number): Promise<Player> {
    const player = await this.get(id)
    return await this.prisma.$transaction(async (prisma) => {
      //delete cards
      await this.cardService.deleteManyFromHand(player.hand.id)
      //delete deck
      return this.prisma.player.delete({
        where: { id },
      });
    })
  }
}
