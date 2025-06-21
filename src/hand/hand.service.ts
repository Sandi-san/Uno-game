import { BadRequestException, Injectable } from '@nestjs/common';
import { Card, Hand, Player, Prisma } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import { CreateHandDto } from './dto/create-hand.dto';
import { CreateCardDto } from 'src/card/dto/create-card.dto';
import { PlayerService } from 'src/player/player.service';
import { CreatePlayerDto } from 'src/player/dto/create-player.dto';
import { UpdatePlayerDto } from 'src/player/dto/update-player.dto';
import { CardService } from 'src/card/card.service';

@Injectable()
export class HandService {
  constructor(
    private prisma: PrismaService,
    private cardService: CardService,
  ) { }

  async create(data: CreateHandDto, playerId: number): Promise<Hand> {
    //NEEDS playerID!!
    return this.prisma.hand.create({
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

  //update hands for specific game 
  async updateForGame(
    dtoPlayers: UpdatePlayerDto[],
    gamePlayers: (Player & { hand: Hand & { cards: Card[] } })[],
  ): Promise<void> {
    for (const playerDto of dtoPlayers) {
      const player = gamePlayers.find(p => p.id === playerDto.id);
      if (!player) throw new BadRequestException(`Player with id ${playerDto.id} not found`);

      //skip current player if no hand update is provided (prevents unlawful deletion of Cards within Hand)
      if (!playerDto.hand || !Array.isArray(playerDto.hand.cards)) continue;

      const newCardIds = playerDto.hand.cards
        .filter(card => card && card.id !== undefined)
        .map(card => card.id);
      const existingCardIds = player.hand.cards
        .filter(card => card && card.id !== undefined)
        .map(card => card.id);


      console.log(`PLAYER HAND: ${playerDto.id} | ${playerDto.hand.cards}`)
      console.log(`HAND NEW: ${newCardIds} | EXISTING: ${existingCardIds}`)

      // Cards removed from this hand - set handId to null
      const toRemove = existingCardIds.filter(id => !newCardIds.includes(id));
      if (toRemove.length > 0) {
        await this.prisma.card.updateMany({
          where: { id: { in: toRemove } },
          data: { handId: null },
        });
      }

      // Cards newly added to this hand - set their handId and remove from deck
      const toAdd = newCardIds.filter(id => !existingCardIds.includes(id));
      if (toAdd.length > 0) {
        await this.prisma.card.updateMany({
          where: { id: { in: toAdd } },
          data: { handId: player.hand.id, deckId: null },
        });
      }

      // Also update indexFirst and indexLast if defined
      const { indexFirst, indexLast } = playerDto.hand;
      const updateHandData: Partial<Hand> = {};
      if (indexFirst !== undefined) updateHandData.indexFirst = indexFirst;
      if (indexLast !== undefined) updateHandData.indexLast = indexLast;

      if (Object.keys(updateHandData).length > 0) {
        await this.prisma.hand.update({
          where: { id: player.hand.id },
          data: updateHandData,
        });
      }
    }
  }

  async delete(id: number): Promise<Hand> {
    return await this.prisma.$transaction(async (prisma) => {
      //delete cards
      await this.cardService.deleteManyFromHand(id)
      //delete deck
      return this.prisma.hand.delete({
        where: { id },
      });
    })
  }
}
