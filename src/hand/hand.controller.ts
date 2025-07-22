import { Body, Controller, Delete, Get, Param, ParseIntPipe, Patch, Post } from '@nestjs/common';
import { HandService } from './hand.service';
import { Hand, Prisma } from '@prisma/client';
import { CreateHandDto } from './dto/create-hand.dto';

@Controller('hand')
export class HandController {
    constructor(private readonly handService: HandService) {}
    /*
    @Post()
    async createHand(@Body() data: CreateHandDto, playerId: number): Promise<Hand> {
      console.log("HAND CREATE:",data)
      return this.handService.create(data, playerId);
    }
    @Get(':id')
    async getHand(@Param('id', ParseIntPipe) id: number): Promise<Hand> {
      return this.handService.get(id);
    }
    @Patch(':id')
    async updateHand(@Param('id', ParseIntPipe) playerId: number, @Body() data: CreateHandDto): Promise<Hand> {
      console.log("HAND UPDATE:",data)
      return this.handService.update(data, playerId);
    }
    @Delete(':id')
    async deleteHand(@Param('id', ParseIntPipe) id: number): Promise<Hand> {
      return this.handService.delete(id);
    }
    */
}
