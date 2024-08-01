import { Body, Controller, Delete, Get, Param, Patch, Post } from '@nestjs/common';
import { CardService } from './card.service';
import { Card, Prisma } from '@prisma/client';

@Controller('card')
export class CardController {
    constructor(private readonly cardService: CardService) {}

    @Post()
    async createPlayer(@Body() data: Prisma.CardCreateInput): Promise<Card> {
      return this.cardService.create(data);
    }
  
    @Get(':id')
    async getPlayer(@Param('id') id: string): Promise<Card> {
      return this.cardService.get(id);
    }
  
    @Patch(':id')
    async updatePlayer(@Param('id') id: string, @Body() data: Prisma.CardUpdateInput): Promise<Card> {
      return this.cardService.update(id, data);
    }
  
    @Delete(':id')
    async deletePlayer(@Param('id') id: string): Promise<Card> {
      return this.cardService.delete(id);
    }
}
