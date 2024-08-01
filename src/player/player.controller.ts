import { Body, Controller, Delete, Get, Param, Patch, Post } from '@nestjs/common';
import { PlayerService } from './player.service';
import { Player, Prisma } from '@prisma/client';

@Controller('player')
export class PlayerController {
    constructor(private readonly playerService: PlayerService) {}

    @Post()
    async createPlayer(@Body() data: Prisma.PlayerCreateInput): Promise<Player> {
      return this.playerService.create(data);
    }
  
    @Get(':id')
    async getPlayer(@Param('id') id: string): Promise<Player> {
      return this.playerService.get(id);
    }
  
    @Patch(':id')
    async updatePlayer(@Param('id') id: string, @Body() data: Prisma.PlayerUpdateInput): Promise<Player> {
      return this.playerService.update(id, data);
    }
  
    @Delete(':id')
    async deletePlayer(@Param('id') id: string): Promise<Player> {
      return this.playerService.delete(id);
    }
}