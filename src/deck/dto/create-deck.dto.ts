import { CreateCardDto } from 'src/card/dto/create-card.dto';

export class CreateDeckDto {
  size: number;
  cards: CreateCardDto[];
  gameId: number;
}