import { Injectable } from '@nestjs/common';
import { Deck, Prisma } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import { DeckDto } from './dto/create-deck.dto';
import { ObjectId } from 'mongodb';

@Injectable()
export class DeckService {
    constructor(private prisma: PrismaService){}

    async create(data: Prisma.DeckCreateInput): Promise<Deck>{
        return this.prisma.deck.create({
            data
        })
    }

    async createMany(gameId: string, decks: DeckDto[]): Promise<void> {
      for (const deck of decks) {
        await this.prisma.deck.create({
          data: {
            gameId: new ObjectId(gameId).toHexString(),
            size: deck.size,
            cards: {
              create: deck.cards.map(card => ({
                priority: card.priority,
                value: card.value,
                color: card.color,
                texture: card.texture,
                position: card.position,
                bounds: card.bounds,
                isHighlighted: card.isHighlighted,
                hand: null,
              })),
            },
          },
        });
      }
    }
  

    async get(id: string): Promise<Deck | null> {
        return this.prisma.deck.findUnique({
          where: { id },
        });
      }
    
      async update(id: string, data: Prisma.DeckUpdateInput): Promise<Deck> {
        return this.prisma.deck.update({
          where: { id },
          data,
        });
      }
    
      async delete(id: string): Promise<Deck> {
        return this.prisma.deck.delete({
          where: { id },
        });
      }
}
