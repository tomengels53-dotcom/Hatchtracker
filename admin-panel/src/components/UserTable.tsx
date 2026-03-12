import React, { useState, useEffect } from 'react';
import { getFirestore, collection, query, limit, onSnapshot } from 'firebase/firestore';
import { getFunctions, httpsCallable } from 'firebase/functions';

/**
 * User Management Table
 * 
 * Logic:
 * - Read-only list of users from Firestore.
 * - Actions (Grant Role, Force Tier) are behind confirmation modals.
 * - All writes go through httpsCallable functions.
 */
const UserTable: React.FC = () => {
    const [users, setUsers] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const db = getFirestore();
    const functions = getFunctions();

    useEffect(() => {
        const q = query(collection(db, 'users'), limit(50));
        const unsubscribe = onSnapshot(q, (snapshot) => {
            const userList = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
            setUsers(userList);
            setLoading(false);
        });
        return () => unsubscribe();
    }, [db]);

    const handleGrantTester = async (uid: string) => {
        if (!window.confirm("Grant 30 days of Expert access to this user?")) return;

        const forceUpdate = httpsCallable(functions, 'adminForceSubscriptionUpdate');
        try {
            await forceUpdate({ targetUid: uid, tier: 'expert', expiryDays: 30 });
            alert("Tester access granted.");
        } catch (err: any) {
            alert(`Error: ${err.message}`);
        }
    };

    if (loading) return <div>Loading users...</div>;

    return (
        <div className="admin-table-container">
            <h2>User Management</h2>
            <table className="admin-table">
                <thead>
                    <tr>
                        <th>UID</th>
                        <th>Email</th>
                        <th>Tier</th>
                        <th>Roles</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {users.map(user => (
                        <tr key={user.id}>
                            <td><code>{user.id.substring(0, 8)}...</code></td>
                            <td>{user.email || 'N/A'}</td>
                            <td><span className={`badge tier-${user.subscriptionTier}`}>{user.subscriptionTier}</span></td>
                            <td>{JSON.stringify(user.roles || {})}</td>
                            <td>
                                <button onClick={() => handleGrantTester(user.id)} className="btn-secondary">
                                    Grant Tester
                                </button>
                                {/* Future: View Invoices, Ban, etc. */}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default UserTable;
