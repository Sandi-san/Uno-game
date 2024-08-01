import { Body, Controller, Delete, Get, Param, Patch, Post } from '@nestjs/common';
import { HandService } from './hand.service';
import { Hand, Prisma } from '@prisma/client';

@Controller('hand')
export class HandController {
    constructor(private readonly handService: HandService) {}

    @Post()
    async createHand(@Body() data: Prisma.HandCreateInput): Promise<Hand> {
      return this.handService.create(data);
    }
  
    @Get(':id')
    async getHand(@Param('id') id: string): Promise<Hand> {
      return this.handService.get(id);
    }
  
    @Patch(':id')
    async updateHand(@Param('id') id: string, @Body() data: Prisma.HandUpdateInput): Promise<Hand> {
      return this.handService.update(id, data);
    }
  
    @Delete(':id')
    async deleteHand(@Param('id') id: string): Promise<Hand> {
      return this.handService.delete(id);
    }
}
