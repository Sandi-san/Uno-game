import { Module } from '@nestjs/common';
import { HandService } from './hand.service';
import { HandController } from './hand.controller';
import { PlayerModule } from 'src/player/player.module';
import { CardModule } from 'src/card/card.module';

@Module({
  imports: [PlayerModule, CardModule],
  providers: [HandService],
  controllers: [HandController],
  exports: [HandService],
})
export class HandModule {}
