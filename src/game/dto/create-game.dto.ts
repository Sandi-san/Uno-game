// create-game.dto.ts

import { DeckDto } from 'src/deck/dto/create-deck.dto';
import { PlayerDto } from 'src/player/dto/create-player.dto';

export class CreateGameDto {
  decks: DeckDto[];
  players: PlayerDto[];
}