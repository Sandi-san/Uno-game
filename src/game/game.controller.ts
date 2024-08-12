import { Controller, Get, Post, Put, Delete, Body, Param, ParseIntPipe } from '@nestjs/common';
import { GameService } from './game.service';
import { Game, Player, Prisma } from '@prisma/client';
import { CreateGameDto } from './dto/create-game.dto';

@Controller('game')
export class GameController {
    constructor(private readonly gameService: GameService) {}

    @Post()
    async createGame(@Body() data: CreateGameDto): Promise<Game> {
      console.log("GAME CREATE:",data);
      //console.log("DECK 1:",data.decks[0].cards);
      //console.log("DECK 2:",data.decks[1].cards);
      return this.gameService.create(data);
    }
  
    @Get()
    async getGames(): Promise<Game[]> {
      return this.gameService.getAll();
    }

    @Get(':id')
    async getGame(@Param('id') id: number): Promise<Game> {
      return this.gameService.get(id);
    }
    
    @Get(':id/players')
    async getGamePlayers(@Param('id', ParseIntPipe) id: number): Promise<Player[]> {
      console.log("FETCH PLAYERS OF GAME:",id);
      return this.gameService.getPlayers(id);
    }
  
    @Put(':id')
    async updateGame(@Param('id') id: number, @Body() data: Prisma.GameUpdateInput): Promise<Game> {
      return this.gameService.update(id, data);
    }
  
    @Delete(':id')
    async deleteGame(@Param('id') id: number): Promise<Game> {
      return this.gameService.delete(id);
    }
}
