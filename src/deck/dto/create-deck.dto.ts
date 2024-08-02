import { CardDto } from 'src/card/dto/create-card.dto';

export class DeckDto {
  size: number;
  cards: CardDto[];
  gameId: number;
}