import { Injectable } from '@nestjs/common';
import { Card, Prisma } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import { CreateCardDto } from './dto/create-card.dto';


@Injectable()
export class CardService {
  constructor(private prisma: PrismaService) { }

  async create(data: CreateCardDto): Promise<Card> {
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

  //delete many cards connected to game 
  async deleteManyFromGame(gameId: number): Promise<void> {
    console.log("Deleting Cards of Game:", gameId)
    await this.prisma.card.deleteMany({
      where: {
        deck: {
          gameId
        }
      }
    })
  }

  //delete many cards connected to deck 
  async deleteManyFromDeck(deckId: number): Promise<void> {
    console.log("Deleting Cards of Deck:", deckId)
    await this.prisma.card.deleteMany({
      where: {
        deck: {
          id: deckId
        }
      }
    })
  }

  //delete many cards connected to hand 
  async deleteManyFromHand(handId: number): Promise<void> {
    console.log("Deleting Cards of Hand:", handId)
    await this.prisma.card.deleteMany({
      where: {
        handId: handId,
      }
    })
  }

  //delete handId from cards but dont delete them
  async disconnectManyFromHand(cardIds: { id: number }[]): Promise<void> {
    console.log("Disconnecting handId of Cards:", cardIds)
    const ids = cardIds.map(c => c.id);

    await this.prisma.card.updateMany({
      where: {
        id: { in: ids },
      },
      data: {
        handId: null,
      },
    });
  }

}
