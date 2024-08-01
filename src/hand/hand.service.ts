import { Injectable } from '@nestjs/common';
import { Hand, Prisma } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';

@Injectable()
export class HandService {
    constructor(private prisma: PrismaService){}

    async create(data: Prisma.HandCreateInput): Promise<Hand> {
        return this.prisma.hand.create({
            data
        })
    }

    async get(id: string): Promise<Hand | null> {
        return this.prisma.hand.findUnique({
          where: { id },
        });
      }
    
      async update(id: string, data: Prisma.HandUpdateInput): Promise<Hand> {
        return this.prisma.hand.update({
          where: { id },
          data,
        });
      }
    
      async delete(id: string): Promise<Hand> {
        return this.prisma.hand.delete({
          where: { id },
        });
      }
}
