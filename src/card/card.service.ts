import { Injectable } from '@nestjs/common';
import { Card, Prisma } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';

@Injectable()
export class CardService {
    constructor(private prisma: PrismaService){}

    async create(data: Prisma.CardCreateInput): Promise<Card>{
        return this.prisma.card.create({
            data
        })
    }

    async get(id: string): Promise<Card | null> {
        return this.prisma.card.findUnique({
          where: { id },
        });
      }
    
      async update(id: string, data: Prisma.CardUpdateInput): Promise<Card> {
        return this.prisma.card.update({
          where: { id },
          data,
        });
      }
    
      async delete(id: string): Promise<Card> {
        return this.prisma.card.delete({
          where: { id },
        });
      }
}
