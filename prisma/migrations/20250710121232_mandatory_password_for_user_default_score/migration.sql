/*
  Warnings:

  - Made the column `password` on table `Player` required. This step will fail if there are existing NULL values in that column.

*/
-- AlterTable
ALTER TABLE "Player" ALTER COLUMN "password" SET NOT NULL;
