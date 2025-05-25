-- DropForeignKey
ALTER TABLE "Card" DROP CONSTRAINT "Card_handId_fkey";

-- DropForeignKey
ALTER TABLE "Deck" DROP CONSTRAINT "Deck_gameId_fkey";

-- DropForeignKey
ALTER TABLE "Hand" DROP CONSTRAINT "Hand_playerId_fkey";

-- AddForeignKey
ALTER TABLE "Deck" ADD CONSTRAINT "Deck_gameId_fkey" FOREIGN KEY ("gameId") REFERENCES "Game"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Hand" ADD CONSTRAINT "Hand_playerId_fkey" FOREIGN KEY ("playerId") REFERENCES "Player"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Card" ADD CONSTRAINT "Card_handId_fkey" FOREIGN KEY ("handId") REFERENCES "Hand"("id") ON DELETE CASCADE ON UPDATE CASCADE;
