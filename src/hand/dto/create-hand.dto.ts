import { CardDto } from 'src/card/dto/create-card.dto';

export class HandDto {
  indexFirst: number;
  indexLast: number;
  cards: CardDto[];
}