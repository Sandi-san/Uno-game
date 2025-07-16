import { Body, Controller, Delete, Get, Param, ParseIntPipe, Patch, Post, Req, UseGuards } from '@nestjs/common';
import { PlayerService } from './player.service';
import { Player, Prisma } from '@prisma/client';
import { CreatePlayerDto } from './dto/create-player.dto';
import { UpdatePlayerDto } from './dto/update-player.dto';
import { JwtAuthGuard } from 'src/auth/jwt';

@Controller('player')
export class PlayerController {
  constructor(private readonly playerService: PlayerService) { }

  //TODO: nekje to use jwt token: npr ustvari game, join game (room join)

  /*
  @Post()
  async createPlayer(@Body() data: CreatePlayerDto): Promise<Player> {
    console.log("PLAYER CREATE:",data)
    return this.playerService.create(data);
  }
  */

  //NOTE: routes with parameters should always be above base routes
  @Get('scores')
  async getPlayersScores(): Promise<Player[]> {
    return this.playerService.getPlayersScores();
  }

  @Get('name/:name')
  async getPlayerByName(@Param('name') name: string): Promise<Player> {
    console.log("PLAYER FETCH:", name)
    return this.playerService.getByName(name);
  }

  @Get(':id')
  async getPlayer(@Param('id', ParseIntPipe) id: number): Promise<Player> {
    return this.playerService.get(id);
  }

  /** Get Player by authentication token */
  @UseGuards(JwtAuthGuard)
  @Get('')
  async get(@Req() req): Promise<Player> {
    const user = req.user
    delete user.password
    return user
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