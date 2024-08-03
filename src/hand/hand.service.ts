import { Injectable } from '@nestjs/common';
import { Hand, Prisma } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import { HandDto } from './dto/create-hand.dto';
import { CardDto } from 'src/card/dto/create-card.dto';
import { PlayerService } from 'src/player/player.service';
import { PlayerDto } from 'src/player/dto/create-player.dto';

@Injectable()
export class HandService {
    constructor(
      private prisma: PrismaService,
      private playerService: PlayerService,
    ){}

    async create(data: HandDto, playerId: number): Promise<Hand> {
      //NEEDS playerID!!
      return this.prisma.hand.create({
            data:{
              cards: {
                create: data.cards
                .filter((card: CardDto | null) => card !== null) // Filter out null values
                .map((card: CardDto) => ({
                  priority: card.priority,
                  value: card.value,
                  color: card.color,
                  texture: card.texture,
                }))
              },
              indexFirst: data.indexFirst,
              indexLast: data.indexLast,
              playerId: playerId,
            }
        })
    }

    async get(id: number): Promise<Hand | null> {
        return this.prisma.hand.findUnique({
          where: { id },
        });
      }
    
      async update(id: number, data: Prisma.HandUpdateInput): Promise<Hand> {
        return this.prisma.hand.update({
          where: { id },
          data,
        });
      }
    
      async delete(id: number): Promise<Hand> {
        return this.prisma.hand.delete({
          where: { id },
        });
      }
}
