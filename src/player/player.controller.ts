import { Body, Controller, Delete, Get, Param, ParseIntPipe, Patch, Post } from '@nestjs/common';
import { PlayerService } from './player.service';
import { Player, Prisma } from '@prisma/client';
import { CreatePlayerDto } from './dto/create-player.dto';
import { UpdatePlayerDto } from './dto/update-player.dto';

@Controller('player')
export class PlayerController {
  constructor(private readonly playerService: PlayerService) { }

  @Get('scores')
  async getPlayersScores(): Promise<Player[]> {
    return this.playerService.getPlayersScores();
  }

  /*
  @Post()
  async createPlayer(@Body() data: CreatePlayerDto): Promise<Player> {
    console.log("PLAYER CREATE:",data)
    return this.playerService.create(data);
  }
  */

  @Get(':id')
  async getPlayer(@Param('id', ParseIntPipe) id: number): Promise<Player> {
    return this.playerService.get(id);
  }

  @Get('name/:name')
  async getPlayerByName(@Param('name') name: string): Promise<Player> {
    console.log("PLAYER FETCH:", name)
    return this.playerService.getByName(name);
  }

  /*
  @Patch(':id')
  async updatePlayer(@Param('id', ParseIntPipe) id: number, @Body() data: UpdatePlayerDto): Promise<Player> {
    return this.playerService.update(id, data);
  }
  */

  @Delete(':id')
  async deletePlayer(@Param('id', ParseIntPipe) id: number): Promise<Player> {
    return this.playerService.delete(id);
  }
}