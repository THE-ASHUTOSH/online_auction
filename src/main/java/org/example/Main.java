package org.example;

import java.sql.*;
import java.util.InputMismatchException;
import java.util.Scanner;


public class Main {
    public static void main(String[] args) {
        String url = "jdbc:mysql://127.0.0.1:3306/online_auction";
        String username = "root";
        String password = "root";
        int choice = 0;
        Scanner scan = new Scanner(System.in);
        try {
            Connection connection = DriverManager.getConnection(url, username, password);
            System.out.println("Connected to MySQL database successfully!");

            while(choice!=4){

                try {
                    printOptions();
                    choice = scan.nextInt();
                    scan.nextLine();
                    switch (choice) {
                        case 1:
                            findCurrentHighestBid(connection, scan);
                            break;
                        case 2:
                            showBiddingHistory(connection, scan);
                            break;
                        case 3:
                            findWinningBidder(connection,scan);
                            break;
                        case 4:
                            System.out.println("Exiting.");
                            break;
                        default:
                            System.out.println("Invalid choice. Retry.");

                    }
                }catch (InputMismatchException e){
                    System.out.println("Invalid Input. Please enter number");
                    scan.next(); // Clear the invalid input
                }
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    public static void printOptions(){
        System.out.println("Choose the task you want to perform");
        System.out.println("1. Find Current Highest Bid for an Item");
        System.out.println("2. Show Bidding History for an Item");
        System.out.println("3. Find Winning Bidder for an Auction");
        System.out.println("4. Exit");
    }

    private static void findCurrentHighestBid(Connection connection, Scanner scanner) throws SQLException {
        System.out.println("Find Current Highest Bid");
        System.out.print("Enter Item ID: ");
        String itemId = scanner.nextLine();

        String selectHighestBidSQL = "SELECT " +
                "a.auction_id, i.title, i.description, a.start_price, a.reserve_price, " +
                "b.bid_amount, b.bid_time, u.name AS bidder_name " +
                "FROM `auction` a " +
                "JOIN `items` i ON a.item_id = i.item_id " +
                "LEFT JOIN `bid_logs` b ON a.current_highest_bid_id = b.bid_id " +
                "LEFT JOIN `users` u ON b.bidder_id = u.user_id " +
                "WHERE a.item_id = ?";

        try (PreparedStatement pstate = connection.prepareStatement(selectHighestBidSQL)) {
            pstate.setString(1, itemId);
            ResultSet rs = pstate.executeQuery();

            if (rs.next()) {
                System.out.println("\n Item Details");
                System.out.println("Item Title: " + rs.getString("title"));
                System.out.println("Item Description: " + rs.getString("description"));
                System.out.println("Auction ID: " + rs.getString("auction_id"));
                System.out.println("Start Price: " + rs.getLong("start_price"));
                System.out.println("Reserve Price: " + rs.getLong("reserve_price"));

                Long bidAmount = rs.getObject("bid_amount", Long.class); // Use getObject for nullable BIGINT
                if (bidAmount != null) {
                    System.out.println("Current Highest Bid: " + bidAmount);
                    System.out.println("Bidder: " + rs.getString("bidder_name"));
                    System.out.println("Bid Time: " + rs.getTimestamp("bid_time"));
                } else {
                    System.out.println("No bids placed yet for this item.");
                    System.out.println("Current Highest Bid: " + rs.getLong("start_price") + " (Start Price)");
                }
            } else {
                System.out.println("No auction found for Item ID: " + itemId);
            }
        }
    }


    private static void showBiddingHistory(Connection connection, Scanner scanner) throws SQLException {
        System.out.println("\n Show Bidding History");
        System.out.print("Enter Item ID: ");
        String itemId = scanner.nextLine();

        String selectBiddingHistorySQL = "SELECT " +
                "b.bid_id, u.name AS bidder_name, b.bid_amount, b.bid_time, b.type " +
                "FROM `bid_logs` b " +
                "JOIN `auction` a ON b.auction_id = a.auction_id " +
                "JOIN `users` u ON b.bidder_id = u.user_id " +
                "WHERE a.item_id = ? " +
                "ORDER BY b.bid_time ASC";

        try (PreparedStatement pstate = connection.prepareStatement(selectBiddingHistorySQL)) {
            pstate.setString(1, itemId);
            ResultSet rs = pstate.executeQuery();

            System.out.println("\n--- Bidding History for Item ID: " + itemId + " ---");
            boolean foundBids = false;
            while (rs.next()) {
                foundBids = true;
                System.out.println("  Bid ID: " + rs.getString("bid_id"));
                System.out.println("  Bidder: " + rs.getString("bidder_name"));
                System.out.println("  Amount: " + rs.getLong("bid_amount"));
                System.out.println("  Time: " + rs.getTimestamp("bid_time"));
                System.out.println("  Type: " + rs.getString("type"));
            }
            if (!foundBids) {
                System.out.println("No bidding history found for this item.");
            }
        }
    }

    /**
     * Finds and displays the winning bidder for a completed auction.
     *
     * @param connection The database connection.
     * @param scanner The Scanner object for user input.
     * @throws SQLException If a database error occurs.
     */
    private static void findWinningBidder(Connection connection, Scanner scanner) throws SQLException {
        System.out.println("\n Find Winning Bidder");
        System.out.print("Enter Auction ID: ");
        String auctionId = scanner.nextLine();

        String selectWinnerSQL = "SELECT " +
                "w.auction_id, u.name AS winner_name, b.bid_amount AS winning_bid_amount, w.confirmed_at, " +
                "t.payment_id, t.status AS payment_status " +
                "FROM `winner` w " +
                "JOIN `users` u ON w.bidder_id = u.user_id " +
                "JOIN `bid_logs` b ON w.bid_id = b.bid_id " +
                "JOIN `Transactions` t ON w.payment_id = t.payment_id " +
                "WHERE w.auction_id = ?";

        try (PreparedStatement pstate = connection.prepareStatement(selectWinnerSQL)) {
            pstate.setString(1, auctionId);
            ResultSet rs = pstate.executeQuery();

            if (rs.next()) {
                System.out.println("\n Winning Bidder Details");
                System.out.println("Auction ID: " + rs.getString("auction_id"));
                System.out.println("Winner Name: " + rs.getString("winner_name"));
                System.out.println("Winning Bid Amount: " + rs.getLong("winning_bid_amount"));
                System.out.println("Confirmed At: " + rs.getTimestamp("confirmed_at"));
                System.out.println("Payment ID: " + rs.getString("payment_id"));
                System.out.println("Payment Status: " + rs.getString("payment_status"));
            } else {
                System.out.println("No winning bidder found for Auction ID: " + auctionId + ". It might be active, pending, or not yet concluded.");
            }
        }
    }


}