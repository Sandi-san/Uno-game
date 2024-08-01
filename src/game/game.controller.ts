import { Controller, Get, Post, Put, Delete, Body, Param } from '@nestjs/common';
import { GameService } from './game.service';
import { Game, Prisma } from '@prisma/client';
import { CreateGameDto } from './dto/create-game.dto';

@Controller('game')
export class GameController {
    constructor(private readonly gameService: GameService) {}

    @Post()
    async createGame(@Body() data: CreateGameDto): Promise<Game> {
      console.log("DATA:",data);
      console.log("DECKS:",data.decks);
      console.log("PLAYERS:",data.players);
      return this.gameService.create(data);
    }
  
    @Get(':id')
    async getGame(@Param('id') id: string): Promise<Game> {
      return this.gameService.get(id);
    }
  
    @Put(':id')
    async updateGame(@Param('id') id: string, @Body() data: Prisma.GameUpdateInput): Promise<Game> {
      return this.gameService.update(id, data);
    }
  
    @Delete(':id')
    async deleteGame(@Param('id') id: string): Promise<Game> {
      return this.gameService.delete(id);
    }
}
