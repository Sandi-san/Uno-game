import { HandDto } from 'src/hand/dto/create-hand.dto'

export class PlayerDto {
  name: string;
  score: number;
  hand: HandDto;
}