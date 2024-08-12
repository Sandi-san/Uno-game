import { Body, Controller, Delete, Get, Param, Patch, Post } from '@nestjs/common';
import { PlayerService } from './player.service';
import { Player, Prisma } from '@prisma/client';
import { PlayerDto } from './dto/create-player.dto';

@Controller('player')
export class PlayerController {
    constructor(private readonly playerService: PlayerService) {}

    @Post()
    async createPlayer(@Body() data: PlayerDto): Promise<Player> {
      console.log("PLAYER CREATE:",data)
      return this.playerService.create(data);
    }
  
    @Get(':id')
    async getPlayer(@Param('id') id: number): Promise<Player> {
      return this.playerService.get(id);
    }
  
    @Get('name/:name')
    async getPlayerByName(@Param('name') name: string): Promise<Player> {
      console.log("PLAYER FETCH:",name)
      return this.playerService.getByName(name);
    }

    @Patch(':id')
    async updatePlayer(@Param('id') id: number, @Body() data: Prisma.PlayerUpdateInput): Promise<Player> {
      return this.playerService.update(id, data);
    }
  
    @Delete(':id')
    async deletePlayer(@Param('id') id: number): Promise<Player> {
      return this.playerService.delete(id);
    }
}