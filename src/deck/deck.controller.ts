import { Body, Controller, Delete, Get, Param, ParseIntPipe, Patch, Post } from '@nestjs/common';
import { DeckService } from './deck.service';
import { Deck, Prisma } from '@prisma/client';
import { CreateDeckDto } from './dto/create-deck.dto';
import { UpdateDeckDto } from './dto/update-deck.dto';

@Controller('deck')
export class DeckController {
    constructor(private readonly deckService: DeckService) {}
    /*
    @Post()
    async createDeck(@Body() data: CreateDeckDto): Promise<Deck> {
      return this.deckService.create(data);
    }
    @Get(':id')
    async getDeck(@Param('id', ParseIntPipe) id: number): Promise<Deck> {
      return this.deckService.get(id);
    }
    @Patch(':id')
    async updateDeck(@Param('id', ParseIntPipe) id: number, @Body() data: UpdateDeckDto): Promise<Deck> {
      return this.deckService.update(id, data);
    }
    @Delete(':id')
    async deleteDeck(@Param('id', ParseIntPipe) id: number): Promise<Deck> {
      return this.deckService.delete(id);
    }
    */
}
