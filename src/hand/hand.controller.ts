import { Body, Controller, Delete, Get, Param, Patch, Post } from '@nestjs/common';
import { HandService } from './hand.service';
import { Hand, Prisma } from '@prisma/client';
import { HandDto } from './dto/create-hand.dto';

@Controller('hand')
export class HandController {
    constructor(private readonly handService: HandService) {}

    @Post()
    async createHand(@Body() data: HandDto, id: number): Promise<Hand> {
      console.log("HAND CREATE:",data)
      return this.handService.create(data, id);
    }
  
    @Get(':id')
    async getHand(@Param('id') id: number): Promise<Hand> {
      return this.handService.get(id);
    }
  
    @Patch(':id')
    async updateHand(@Param('id') id: number, @Body() data: Prisma.HandUpdateInput): Promise<Hand> {
      return this.handService.update(id, data);
    }
  
    @Delete(':id')
    async deleteHand(@Param('id') id: number): Promise<Hand> {
      return this.handService.delete(id);
    }
}
