import { Body, Controller, Delete, Get, Param, Patch, Post } from '@nestjs/common';
import { DeckService } from './deck.service';
import { Deck, Prisma } from '@prisma/client';

@Controller('deck')
export class DeckController {
    constructor(private readonly deckService: DeckService) {}

    @Post()
    async createDeck(@Body() data: Prisma.DeckCreateInput): Promise<Deck> {
      return this.deckService.create(data);
    }
  
    @Get(':id')
    async getDeck(@Param('id') id: string): Promise<Deck> {
      return this.deckService.get(id);
    }
  
    @Patch(':id')
    async updateDeck(@Param('id') id: string, @Body() data: Prisma.DeckUpdateInput): Promise<Deck> {
      return this.deckService.update(id, data);
    }
  
    @Delete(':id')
    async deleteDeck(@Param('id') id: string): Promise<Deck> {
      return this.deckService.delete(id);
    }
}
