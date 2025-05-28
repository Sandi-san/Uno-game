/*
  Warnings:

  - You are about to drop the `_CardToDeck` table. If the table is not empty, all the data it contains will be lost.

*/
-- DropForeignKey
ALTER TABLE "_CardToDeck" DROP CONSTRAINT "_CardToDeck_A_fkey";

-- DropForeignKey
ALTER TABLE "_CardToDeck" DROP CONSTRAINT "_CardToDeck_B_fkey";

-- AlterTable
ALTER TABLE "Card" ADD COLUMN     "deckId" INTEGER;

-- DropTable
DROP TABLE "_CardToDeck";

-- AddForeignKey
ALTER TABLE "Card" ADD CONSTRAINT "Card_deckId_fkey" FOREIGN KEY ("deckId") REFERENCES "Deck"("id") ON DELETE CASCADE ON UPDATE CASCADE;
