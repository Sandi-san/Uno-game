import { BadRequestException, Injectable } from '@nestjs/common';
import { Card, Deck, Game, Prisma } from '@prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';
import { CreateDeckDto } from './dto/create-deck.dto';
import { CreateCardDto } from 'src/card/dto/create-card.dto';
import { UpdateDeckDto } from './dto/update-deck.dto';

@Injectable()
export class DeckService {
  constructor(private prisma: PrismaService) { }

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
      include: {cards: true}
    });

    console.log(`DECK ${updatedDeck.id} ADD: `, cardIds)
    return updatedDeck
  }

  //update decks for specific game 
  async updateForGame(gameId: number, dtoDecks: UpdateDeckDto[], gameDecks: (Deck & { cards: Card[] })[]): Promise<void> {
    for (const deckDto of dtoDecks) {
      const deck = gameDecks.find(d => d.id === deckDto.id);
      if (!deck) throw new BadRequestException(`Deck with id ${deckDto.id} not found`);

      const newCardIds = deckDto.cards
        .filter(card => card && card.id !== undefined)
        .map(card => card.id);
      const existingCardIds = deck.cards
        .filter(card => card && card.id !== undefined)
        .map(card => card.id);

      // Disconnect removed cards
      await this.prisma.deck.update({
        where: { id: deck.id },
        data: {
          cards: {
            disconnect: existingCardIds
              .filter(id => !newCardIds.includes(id)).map(id => ({ id })),
          },
        },
      });

      // Connect new cards
      await this.prisma.deck.update({
        where: { id: deck.id },
        data: {
          cards: {
            connect: newCardIds
              .filter(id => !existingCardIds.includes(id)).map(id => ({ id })),
          },
        },
      });
    }
  }

  async delete(id: number): Promise<Deck> {
    return this.prisma.deck.delete({
      where: { id },
    });
  }
}
