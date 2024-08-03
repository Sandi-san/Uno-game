import { Injectable } from '@nestjs/common';
import { Card, Prisma } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import { CardDto } from './dto/create-card.dto';


@Injectable()
export class CardService {
    constructor(private prisma: PrismaService){}

    async create(data: CardDto): Promise<Card>{
        return this.prisma.card.create({
            data: {
              priority: data.priority,
              value: data.value,
              color: data.color,
              texture: data.texture,
              handId: data.handId,
            }
        })
    }

    async get(id: number): Promise<Card | null> {
        return this.prisma.card.findUnique({
          where: { id },
        });
      }
    
      async update(id: number, data: Prisma.CardUpdateInput): Promise<Card> {
        return this.prisma.card.update({
          where: { id },
          data,
        });
      }
    
      async delete(id: number): Promise<Card> {
        return this.prisma.card.delete({
          where: { id },
        });
      }
}
