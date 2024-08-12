import { HandDto } from 'src/hand/dto/create-hand.dto'

export class PlayerDto {
  id?: number;
  name: string;
  score: number;
  hand?: HandDto;
  gameId?: number;
}