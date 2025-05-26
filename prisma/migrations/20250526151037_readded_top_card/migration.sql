-- AlterTable
ALTER TABLE "Game" ADD COLUMN     "topCardId" INTEGER;

-- AddForeignKey
ALTER TABLE "Game" ADD CONSTRAINT "Game_topCardId_fkey" FOREIGN KEY ("topCardId") REFERENCES "Card"("id") ON DELETE SET NULL ON UPDATE CASCADE;
