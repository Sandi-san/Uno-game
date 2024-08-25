import { BadRequestException, Injectable } from '@nestjs/common'
import { Card, Hand, Player, Prisma } from '@prisma/client'
import { PrismaService } from 'src/prisma/prisma.service'
import { CreatePlayerDto } from './dto/create-player.dto';
import { CreateCardDto } from 'src/card/dto/create-card.dto';
import { UpdatePlayerDto } from './dto/update-player.dto';

@Injectable()
export class PlayerService {
  constructor(private prisma: PrismaService) { }

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

  async get(id: number): Promise<Player &
  {
    hand: (Hand & {cards: Card[]})
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
      throw new BadRequestException(`Player with id ${dto.id} not found.`);

    const handData = {
      indexFirst: dto.hand?.indexFirst ?? 0, // Default to 0 if not provided
      indexLast: dto.hand?.indexLast ?? -1,  // Default to -1 if not provided
      cards: {
        connect: dto.hand?.cards
          ?.filter(card => card && card.id !== undefined)
          .map(card => ({ id: card.id })) || [], // Handle case where cards might be undefined
      },
    };

    const updatedPlayer = await this.prisma.player.update({
      where: {
        id: dto.id,
      },
      data: {
        gameId, //change gameId
        joinedAt: new Date(),
        hand: player.hand ? {
          update: handData,
        } : {
          create: handData,
        },
      },
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

  async delete(id: number): Promise<Player> {
    return this.prisma.player.delete({
      where: { id },
    })
  }
}
