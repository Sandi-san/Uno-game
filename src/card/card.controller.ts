import { Body, Controller, Delete, Get, Param, ParseIntPipe, Patch, Post } from '@nestjs/common';
import { CardService } from './card.service';
import { Card, Prisma } from '@prisma/client';
import { CreateCardDto } from './dto/create-card.dto';

@Controller('card')
export class CardController {
    constructor(private readonly cardService: CardService) {}

    @Post()
    async createCard(@Body() data: CreateCardDto): Promise<Card> {
      console.log("CARD CREATE:",data)
      return this.cardService.create(data);
    }
  
    @Get(':id')
    async getCard(@Param('id', ParseIntPipe) id: number): Promise<Card> {
      return this.cardService.get(id);
    }
  
    @Patch(':id')
    async updateCard(@Param('id', ParseIntPipe) id: number, @Body() data: Prisma.CardUpdateInput): Promise<Card> {
      return this.cardService.update(id, data);
    }
  
    @Delete(':id')
    async deleteCard(@Param('id', ParseIntPipe) id: number): Promise<Card> {
      return this.cardService.delete(id);
    }
}
