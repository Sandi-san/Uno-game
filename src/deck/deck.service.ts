import { Injectable } from '@nestjs/common';
import { Deck, Game, Prisma } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import { CreateDeckDto } from './dto/create-deck.dto';
import { CreateCardDto } from 'src/card/dto/create-card.dto';

@Injectable()
export class DeckService {
    constructor(private prisma: PrismaService){}

    async create(data: CreateDeckDto): Promise<Deck>{
        return this.prisma.deck.create({
            data: {
              cards: {
                create: data.cards
                .filter((card: CreateCardDto | null) => card !== null) // Filter out null values
                .map((card: CreateCardDto) => ({
                  priority: card.priority,
                  value: card.value,
                  color: card.color,
                  texture: card.texture,
                }))
              },
              gameId: data.gameId,
              size: data.size
            }
        })
    }

    async get(id: number): Promise<Deck | null> {
        return this.prisma.deck.findUnique({
          where: { id },
        });
      }
    
      async update(id: number, data: Prisma.DeckUpdateInput): Promise<Deck> {
        return this.prisma.deck.update({
          where: { id },
          data,
        });
      }
    
      async delete(id: number): Promise<Deck> {
        return this.prisma.deck.delete({
          where: { id },
        });
      }
}
