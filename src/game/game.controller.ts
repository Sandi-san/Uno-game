import { Controller, Get, Post, Delete, Body, Param, ParseIntPipe, Patch, Put } from '@nestjs/common';
import { GameService } from './game.service';
import { Game, Player, Prisma } from '@prisma/client';
import { CreateGameDto } from './dto/create-game.dto';
import { UpdateGameDto } from './dto/update-game.dto';
import { UpdatePlayerDto } from 'src/player/dto/update-player.dto';
import { plainToInstance } from 'class-transformer';
import { UpdatePlayerTurnDto } from 'src/player/dto/update-player-turn.dto';

@Controller('game')
export class GameController {
  constructor(private readonly gameService: GameService) { }

  @Post()
  async createGame(@Body() data: CreateGameDto): Promise<Game> {
    console.log("GAME CREATE:", data);
    //console.log("DECK 1:",data.decks[0].cards);
    //console.log("DECK 2:",data.decks[1].cards);
    return this.gameService.create(data);
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
    console.log("FETCH PLAYERS OF GAME:", id);
    return this.gameService.getPlayers(id);
  }

  @Get(':id/turn')
  async getGameTurn(@Param('id', ParseIntPipe) id: number): Promise<Game> {
    //console.log("FETCH TURN OF GAME:", id);
    return this.gameService.getTurnAndTopCard(id);
  }

  //Update game data
  @Put(':id')
  async updateGame(@Param('id', ParseIntPipe) id: number, @Body() data: UpdateGameDto): Promise<Game> {
    console.log("UPDATE DATA:", data)
    return this.gameService.update(id, data);
  }

  //Add player to game, change Player's gameId return the updated Game
  @Put(':id/players')
  async updateGamePlayer(@Param('id', ParseIntPipe) id: number, @Body() data: UpdatePlayerDto): Promise<Game> {
    console.log("UPDATE DATA:", data)
    return this.gameService.updatePlayerAdd(id, data);
  }

  @Put(':gameId/player/:playerId')
  async updateGameRemovePlayer(
    @Param('gameId', ParseIntPipe) gameId: number,
    @Param('playerId', ParseIntPipe) playerId: number, @Body() data: UpdatePlayerDto): Promise<Game> {
    console.log("REMOVE PLAYER DATA:", data)
    console.log("PlayerId:", playerId)
    console.log("GameId:", gameId)
    return this.gameService.updatePlayerRemove(gameId, playerId, data);
  }

  //remove player and update turn
  @Put(':gameId/player/:playerId/turn')
  async updateGameRemovePlayerTurn(
    @Param('gameId', ParseIntPipe) gameId: number,
    @Param('playerId', ParseIntPipe) playerId: number,
    @Body() data: UpdatePlayerTurnDto): Promise<Game> {
    console.log("REMOVE PLAYER DATA:", data)
    console.log("PlayerId:", playerId)
    console.log("GameId:", gameId)
    const updatedGame = this.gameService.updatePlayerRemove(gameId, playerId, data);
    if (updatedGame != null) {
      const updateDto = plainToInstance(UpdateGameDto, {
        currentTurn: data.currentTurn
      });
      console.log("updateDto:", updateDto.currentTurn)
      return this.gameService.update(gameId, updateDto)
    }
    return updatedGame
  }

  @Delete(':id')
  async deleteGame(@Param('id', ParseIntPipe) id: number): Promise<Game> {
    return this.gameService.delete(id);
  }
}
