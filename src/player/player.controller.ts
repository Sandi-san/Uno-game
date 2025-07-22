import { Controller, Delete, Get, Param, ParseIntPipe, Req, UseGuards } from '@nestjs/common';
import { PlayerService } from './player.service';
import { Player } from '@prisma/client';
import { JwtAuthGuard } from 'src/auth/jwt';

@Controller('player')
export class PlayerController {
  constructor(private readonly playerService: PlayerService) { }

  //NOTE: routes with parameters should always be above base routes
  @Get('scores')
  async getPlayersScores(): Promise<Player[]> {
    return this.playerService.getPlayersScores();
  }

  /*@Get(':id')
  async getPlayer(@Param('id', ParseIntPipe) id: number): Promise<Player> {
    return this.playerService.get(id);
  }*/

  /** Get Player by authentication token */
  @UseGuards(JwtAuthGuard)
  @Get('')
  async get(@Req() req): Promise<Player> {
    const user = req.user
    delete user.password
    return user
  }

  /*@Delete(':id')
  async deletePlayer(@Param('id', ParseIntPipe) id: number): Promise<Player> {
    return this.playerService.delete(id);
  }*/
}