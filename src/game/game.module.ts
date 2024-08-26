import { Module } from '@nestjs/common';
import { GameService } from './game.service';
import { GameController } from './game.controller';
import { DeckModule } from 'src/deck/deck.module';
import { PlayerModule } from 'src/player/player.module';
import { HandModule } from 'src/hand/hand.module';
import { CardModule } from 'src/card/card.module';

@Module({
  imports: [DeckModule,PlayerModule,HandModule,CardModule],
  providers: [GameService],
  controllers: [GameController],
  exports: [GameService],
})
export class GameModule {}
