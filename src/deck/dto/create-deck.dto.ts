import { CardDto } from 'src/card/dto/create-card.dto';

export class DeckDto {
  size: number;
  position: object;
  bounds: object;
  cards: CardDto[];
}