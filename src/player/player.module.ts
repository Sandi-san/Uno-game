import { Module } from '@nestjs/common';
import { PlayerService } from './player.service';
import { PlayerController } from './player.controller';
import { PrismaModule } from 'src/prisma/prisma.module';
import { CardModule } from 'src/card/card.module';

@Module({
  imports: [PrismaModule, CardModule],
  providers: [PlayerService],
  controllers: [PlayerController],
  exports: [PlayerService],
})
export class PlayerModule {}
