import { Controller, Get, Post, Delete, Body, Param, ParseIntPipe, Put, UseGuards } from '@nestjs/common';
import { GameService } from './game.service';
import { Game, Player, Prisma } from '@prisma/client';
import { CreateGameDto } from './dto/create-game.dto';
import { UpdateGameDto } from './dto/update-game.dto';
import { UpdatePlayerDto } from 'src/player/dto/update-player.dto';
import { UpdatePlayerTurnDto } from 'src/player/dto/update-player-turn.dto';
import { JwtAuthGuard } from 'src/auth/jwt';

@Controller('game')
export class GameController {
  constructor(private readonly gameService: GameService) { }

  //protect route with Authorized JWT token (Logged User)
  @UseGuards(JwtAuthGuard)
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

  @UseGuards(JwtAuthGuard)
  @Get(':id/turn')
  async getGameTurn(@Param('id', ParseIntPipe) id: number): Promise<Game> {
    //console.log("FETCH TURN OF GAME:", id);
    return this.gameService.getTurnAndTopCard(id);
  }

  //Update game data
  @UseGuards(JwtAuthGuard)
  @Put(':id')
  async updateGame(@Param('id', ParseIntPipe) id: number, @Body() data: UpdateGameDto): Promise<Game> {
    console.log("UPDATE DATA:", data)
    return this.gameService.update(id, data);
  }

  //Add player to game, change Player's gameId return the updated Game
  @UseGuards(JwtAuthGuard)
  @Put(':id/players')
  async updateGamePlayer(@Param('id', ParseIntPipe) id: number, @Body() data: UpdatePlayerDto): Promise<Game> {
    console.log("UPDATE DATA:", data)
    return this.gameService.updatePlayerAdd(id, data);
  }

  @UseGuards(JwtAuthGuard)
  @Put(':gameId/player/:playerId')
  async updateGameRemovePlayer(
    @Param('gameId', ParseIntPipe) gameId: number,
    @Param('playerId', ParseIntPipe) playerId: number,
    @Body() data: UpdatePlayerDto): Promise<Game> {
    console.log("REMOVE PLAYER DATA:", data)
    //console.log("PlayerId:", playerId)
    //console.log("GameId:", gameId)
    return this.gameService.updatePlayerRemove(gameId, playerId, data);
  }

  //remove player and update turn
  @UseGuards(JwtAuthGuard)
  @Put(':gameId/player/:playerId/turn')
  async updateGameRemovePlayerTurn(
    @Param('gameId', ParseIntPipe) gameId: number,
    @Param('playerId', ParseIntPipe) playerId: number,
    @Body() data: UpdatePlayerTurnDto): Promise<Game> {
    console.log("REMOVE PLAYER & CHANGE TURN DATA:", data)
    //console.log("PlayerId:", playerId)
    //console.log("GameId:", gameId)
    const updatedGame = await this.gameService.updatePlayerRemove(gameId, playerId, data);
    if(updatedGame!=null){
      //if currentTurn has been updated as well, trigger turnUpdate on gateway listeners (app calls listener method) 
      await this.gameService.triggerTurnChange(updatedGame.id);
    }
    return updatedGame
  }

  /*
  @UseGuards(JwtAuthGuard)
  @Delete(':id')
  async deleteGame(@Param('id', ParseIntPipe) id: number): Promise<Game> {
    return this.gameService.delete(id);
  }
  */
}
