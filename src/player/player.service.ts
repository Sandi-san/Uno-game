import { Injectable } from '@nestjs/common'
import {Player, Prisma} from '@prisma/client'
import { PrismaService } from 'src/prisma/prisma.service'
import { PlayerDto } from './dto/create-player.dto';
import { ObjectId } from 'mongodb';

@Injectable()
export class PlayerService {
    constructor(private prisma: PrismaService){}

    async create(data: Prisma.PlayerCreateInput): Promise<Player>{
        return this.prisma.player.create({
            data
        })
    }

    async createMany(gameId: string, players: PlayerDto[]): Promise<void> {
      for (const player of players) {
        await this.prisma.player.create({
          data: {
            gameId: new ObjectId(gameId).toHexString(),
            name: player.name,
            score: player.score,
            hand: {
              create: {
                indexFirst: player.hand.indexFirst,
                indexLast: player.hand.indexLast,
                positionArrowRegionLeft: player.hand.positionArrowRegionLeft,
                boundsArrowRegionLeft: player.hand.boundsArrowRegionLeft,
                positionArrowRegionRight: player.hand.positionArrowRegionRight,
                boundsArrowRegionRight: player.hand.boundsArrowRegionRight,
                cards: {
                  create: player.hand.cards.map(card => ({
                    priority: card.priority,
                    value: card.value,
                    color: card.color,
                    texture: card.texture,
                    position: card.position,
                    bounds: card.bounds,
                    isHighlighted: card.isHighlighted,
                    deck: null,
                  })),
                },
              },
            },
          },
        });
      }
    }

    async get(id: string): Promise<Player | null> {
        return this.prisma.player.findUnique({
          where: { id },
        })
      }
    
      async update(id: string, data: Prisma.PlayerUpdateInput): Promise<Player> {
        return this.prisma.player.update({
          where: { id },
          data,
        })
      }
    
      async delete(id: string): Promise<Player> {
        return this.prisma.player.delete({
          where: { id },
        })
      }
}
