/*
  Warnings:

  - You are about to drop the column `bounds` on the `Card` table. All the data in the column will be lost.
  - You are about to drop the column `isHighlighted` on the `Card` table. All the data in the column will be lost.
  - You are about to drop the column `position` on the `Card` table. All the data in the column will be lost.
  - You are about to drop the column `boundsArrowRegionLeft` on the `Hand` table. All the data in the column will be lost.
  - You are about to drop the column `boundsArrowRegionRight` on the `Hand` table. All the data in the column will be lost.
  - You are about to drop the column `positionArrowRegionLeft` on the `Hand` table. All the data in the column will be lost.
  - You are about to drop the column `positionArrowRegionRight` on the `Hand` table. All the data in the column will be lost.

*/
-- AlterTable
ALTER TABLE "Card" DROP COLUMN "bounds",
DROP COLUMN "isHighlighted",
DROP COLUMN "position";

-- AlterTable
ALTER TABLE "Hand" DROP COLUMN "boundsArrowRegionLeft",
DROP COLUMN "boundsArrowRegionRight",
DROP COLUMN "positionArrowRegionLeft",
DROP COLUMN "positionArrowRegionRight";
