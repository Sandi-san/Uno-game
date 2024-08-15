import { Controller, Get, Post, Delete, Body, Param, ParseIntPipe, Patch, Put } from '@nestjs/common';
import { GameService } from './game.service';
import { Game, Player, Prisma } from '@prisma/client';
import { CreateGameDto } from './dto/create-game.dto';
import { UpdateGameDto } from './dto/update-game.dto';

@Controller('game')
export class GameController {
    constructor(private readonly gameService: GameService) {}

    @Post()
    async createGame(@Body() data: CreateGameDto): Promise<Game> {
      console.log("GAME CREATE:",data);
      //console.log("DECK 1:",data.decks[0].cards);
      //console.log("DECK 2:",data.decks[1].cards);
      //return this.gameService.create(data);
      return undefined;
    }
  
    @Get()
    async getGames(): Promise<Game[]> {
      return this.gameService.getAll();
    }

    @Get(':id')
    async getGame(@Param('id', ParseIntPipe) id: number): Promise<Game> {
      return this.gameService.get(id);
    }
    
    @Get(':id/players')
    async getGamePlayers(@Param('id', ParseIntPipe) id: number): Promise<Player[]> {
      console.log("FETCH PLAYERS OF GAME:",id);
      return this.gameService.getPlayers(id);
    }
  
    //NEXT TIME: update z patch, update dto
    //TODO: UPDATE DTO
    @Put(':id')
    async updateGame(@Param('id', ParseIntPipe) id: number, @Body() data: UpdateGameDto): Promise<Game> {
      console.log("UPDATE DATA:",data)
      return this.gameService.update(id, data);
    }
  
    @Delete(':id')
    async deleteGame(@Param('id') id: number): Promise<Game> {
      return this.gameService.delete(id);
    }
}
