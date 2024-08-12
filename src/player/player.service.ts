import { Injectable } from '@nestjs/common'
import { Player, Prisma } from '@prisma/client'
import { PrismaService } from 'src/prisma/prisma.service'
import { PlayerDto } from './dto/create-player.dto';
import { CardDto } from 'src/card/dto/create-card.dto';

@Injectable()
export class PlayerService {
  constructor(private prisma: PrismaService) { }

  async create(data: PlayerDto): Promise<Player> {
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
        //add gameId if exists
        ...(data.gameId !== undefined && {gameId: data.gameId})
      }
    })
    console.log(player)
    return player
  }

  async get(id: number): Promise<Player | null> {
    return this.prisma.player.findUnique({
      where: { id },
      include: {
        hand: true
      }
    })
  }

  async getByName(name: string): Promise<Player | null> {
    const player = await this.prisma.player.findFirst({
      where: { name },
    })
    console.log(player)
    return player
  }

  async update(id: number, data: Prisma.PlayerUpdateInput): Promise<Player> {
    return this.prisma.player.update({
      where: { id },
      data,
    })
  }

  async delete(id: number): Promise<Player> {
    return this.prisma.player.delete({
      where: { id },
    })
  }
}
