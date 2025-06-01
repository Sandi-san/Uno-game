import { BadRequestException, Injectable } from '@nestjs/common';
import { Card, Deck, Game, Prisma } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import { CreateDeckDto } from './dto/create-deck.dto';
import { CreateCardDto } from 'src/card/dto/create-card.dto';
import { UpdateDeckDto } from './dto/update-deck.dto';
import { CardService } from 'src/card/card.service';

@Injectable()
export class DeckService {
  constructor(
    private prisma: PrismaService,
    private cardService: CardService,
  ) { }

  async create(data: CreateDeckDto): Promise<Deck> {
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
      include: {
        cards: true
      }
    });
  }


  async getForGame(gameId: number): Promise<(Deck & { cards: Card[] })[]> {
    return this.prisma.deck.findMany({
      where: { 
        gameId
      },
      include: {
        cards: true,
      }
    });
  }

  async update(id: number, dto: UpdateDeckDto): Promise<Deck> {
    return this.prisma.deck.update({
      where: { id },
      data: {}
    });
  }

  //remove cards defined in the dto
  async updateRemoveCards(dto: UpdateDeckDto): Promise<Deck> {
    const { id: deckId, cards } = dto;

    // Step 1: Get the card ids from the player's hand that need to be removed from the deck
    const cardIdsToRemove = cards.map(card => card.id);

    // Step 2: Update the deck by disconnecting the cards from it
    const updatedDeck = await this.prisma.deck.update({
      where: { id: deckId },
      data: {
        cards: {
          disconnect: cardIdsToRemove.map(cardId => ({ id: cardId })),
        },
      },
    });

    console.log(`DECK ${deckId} REMOVE: `, cardIdsToRemove)
    return updatedDeck
  }

  //add cards defined in the dto
  async updateAddCards(deck: Deck, cards: Card[]): Promise<Deck> {
    const cardIds = cards
      ?.filter(card => card && card.id !== undefined)
      .map(card => ({ id: card.id }))

    const updatedDeck = await this.prisma.deck.update({
      where: { id: deck.id },
      data: {
        cards: {
          connect: cardIds, // Connect the player's hand cards to the draw deck
        },
      },
      include: { cards: true }
    });

    console.log(`DECK ${updatedDeck.id} ADD: `, cardIds)
    return updatedDeck
  }

  //update decks for specific game 
  async updateForGame(dtoDecks: UpdateDeckDto[], gameDecks: (Deck & { cards: Card[] })[]): Promise<void> {
    console.log(`Update Deck Discard ${dtoDecks[1].id}`, dtoDecks[1].cards)
    for (const deckDto of dtoDecks) {
      const deck = gameDecks.find(d => d.id === deckDto.id);
      if (!deck) throw new BadRequestException(`Deck with id ${deckDto.id} not found`);

      const newCardIds = deckDto.cards
        .filter(card => card && card.id !== undefined)
        .map(card => card.id);
      const existingCardIds = deck.cards
        .filter(card => card && card.id !== undefined)
        .map(card => card.id);

      // Cards removed from this deck - set deckId to null
      const toRemove = existingCardIds.filter(id => !newCardIds.includes(id));
      if (toRemove.length > 0) {
        await this.prisma.card.updateMany({
          where: { id: { in: toRemove } },
          data: { deckId: null },
        });
      }

      // Cards newly added to this deck - set their deckId
      const toAdd = newCardIds.filter(id => !existingCardIds.includes(id));
      if (toAdd.length > 0) {
        await this.prisma.card.updateMany({
          where: { id: { in: toAdd } },
          data: { deckId: deck.id, handId: null }, // Important: remove from Hand
        });
      }
    }
  }

  async delete(id: number): Promise<Deck> {
    return await this.prisma.$transaction(async (prisma) => {
      //delete cards
      await this.cardService.deleteManyFromDeck(id)
      //delete deck
      return this.prisma.deck.delete({
        where: { id },
      });
    })
  }

  //delete all decks when deleting game
  async deleteManyFromGame(gameId: number): Promise<void> {
    console.log("Deleting Decks of Game:", gameId)
    await this.prisma.deck.deleteMany({
      where: { gameId: gameId }
    });
  }
}
